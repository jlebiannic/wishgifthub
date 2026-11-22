"""Transport SSH/SFTP depuis le poste local ; n'affiche jamais la clé privée."""
import argparse
import base64
from pathlib import Path
import select
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'rssh/python-tools'))
import paramiko


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--script', type=Path)
    parser.add_argument('--upload', type=Path)
    parser.add_argument('--remote')
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    if not args.script and not (args.upload and args.remote):
        parser.error('Utiliser --script ou --upload avec --remote')
    with paramiko.SSHClient() as client:
        client.load_host_keys(str(Path.home() / '.ssh/known_hosts'))
        client.set_missing_host_key_policy(paramiko.RejectPolicy())
        client.connect('192.168.0.50', username='pi',
                       key_filename=str(ROOT / 'rssh/id_ed25519_raspi'),
                       look_for_keys=False, allow_agent=False, timeout=15,
                       auth_timeout=15, banner_timeout=15)
        if args.upload:
            with client.open_sftp() as sftp:
                sftp.get_channel().settimeout(args.timeout)
                sftp.put(str(args.upload), args.remote, confirm=True)
            print(f'Transfert terminé : {args.remote}', flush=True)
        if args.script:
            script = args.script.read_text(encoding='utf-8').replace('\r\n', '\n')
            encoded = base64.b64encode(script.encode()).decode('ascii')
            channel = client.get_transport().open_session()
            channel.exec_command(f'echo {encoded} | base64 -d | bash')
            channel.shutdown_write()
            deadline = time.monotonic() + args.timeout
            while True:
                if channel.recv_ready():
                    sys.stdout.buffer.write(channel.recv(65536))
                    sys.stdout.buffer.flush()
                if channel.recv_stderr_ready():
                    sys.stderr.buffer.write(channel.recv_stderr(65536))
                    sys.stderr.buffer.flush()
                if channel.exit_status_ready() and not channel.recv_ready() and not channel.recv_stderr_ready():
                    break
                if time.monotonic() > deadline:
                    raise TimeoutError('Vérifier l’état distant avant de réessayer')
                select.select([channel], [], [], 0.2)
            sys.exit(channel.recv_exit_status())


if __name__ == '__main__':
    main()
