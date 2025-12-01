/**
 * Script de correction automatique des fichiers générés par swagger-typescript-api
 *
 * Ce script corrige deux problèmes :
 * 1. Supprime les directives @ts-nocheck qui causent des problèmes de compilation
 * 2. Convertit les imports de types en "import type" pour compatibilité avec verbatimModuleSyntax
 */

import {readFileSync, writeFileSync} from 'fs';
import {join} from 'path';

const API_DIR = 'src/generated/api/wish';

const files = [
  join(API_DIR, 'data-contracts.ts'),
  join(API_DIR, 'Api.ts'),
  join(API_DIR, 'http-client.ts')
];

console.log('🔧 Correction des fichiers générés...\n');

// 1. Supprimer @ts-nocheck dans tous les fichiers
files.forEach(file => {
  try {
    let content = readFileSync(file, 'utf8');
    const original = content;

    content = content.replace(/\/\/ @ts-nocheck\n/g, '');

    if (content !== original) {
      writeFileSync(file, content, 'utf8');
      console.log(`✅ ${file} - @ts-nocheck supprimé`);
    }
  } catch (error) {
    console.error(`❌ Erreur lors du traitement de ${file}:`, error.message);
  }
});

// 2. Corriger les imports dans Api.ts
try {
  const apiFile = join(API_DIR, 'Api.ts');
  let content = readFileSync(apiFile, 'utf8');
  const original = content;

  // Remplacer les imports de types par "import type"
  content = content.replace(
    /import \{([^}]+)\} from "\.\/data-contracts";/,
    'import type {$1} from "./data-contracts";'
  );

  // Séparer ContentType/HttpClient (classes) de RequestParams (type)
  content = content.replace(
    /import \{([^}]*ContentType[^}]*HttpClient[^}]*RequestParams[^}]*)\} from "\.\/http-client";/,
    'import type { RequestParams } from "./http-client";\nimport { ContentType, HttpClient } from "./http-client";'
  );

  if (content !== original) {
    writeFileSync(apiFile, content, 'utf8');
    console.log(`✅ ${apiFile} - Imports corrigés`);
  }
} catch (error) {
  console.error(`❌ Erreur lors de la correction de Api.ts:`, error.message);
}

console.log('\n🎉 Corrections appliquées avec succès !');

