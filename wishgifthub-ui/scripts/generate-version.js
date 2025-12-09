/**
 * Script pour générer un fichier de version avec la date/heure de build
 */
import fs from 'fs';
import path from 'path';
import {fileURLToPath} from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Générer la date/heure actuelle au format ISO
const buildDate = new Date();
const buildTimestamp = buildDate.toISOString();

// Formater pour l'affichage humain
const formatDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

const versionInfo = {
  buildTimestamp: buildTimestamp,
  buildDate: formatDate(buildDate),
  version: process.env.npm_package_version || '1.0.0'
};

// Créer le fichier version.json dans public/
const publicDir = path.join(__dirname, '../public');
const versionFilePath = path.join(publicDir, 'version.json');

// Créer le répertoire public s'il n'existe pas
if (!fs.existsSync(publicDir)) {
  fs.mkdirSync(publicDir, { recursive: true });
}

// Écrire le fichier version.json
fs.writeFileSync(versionFilePath, JSON.stringify(versionInfo, null, 2));

console.log('✅ Fichier version.json généré:');
console.log(JSON.stringify(versionInfo, null, 2));

// Créer également un fichier TypeScript pour l'import
const tsContent = `// Généré automatiquement lors du build
export const VERSION_INFO = ${JSON.stringify(versionInfo, null, 2)} as const;
`;

const srcDir = path.join(__dirname, '../src');
const versionTsPath = path.join(srcDir, 'version.ts');

fs.writeFileSync(versionTsPath, tsContent);
console.log('✅ Fichier version.ts généré');

