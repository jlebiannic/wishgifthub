# !/bin/bash
# Script to install certbot and obtain SSL certificates for a given domain
sudo certbot certonly \
  --webroot -w ./certbot/www \
  -d wishgifthub.com \
  --email jlebiannic@gmail.com \
  --agree-tos \
  --no-eff-email
