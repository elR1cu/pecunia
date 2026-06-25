// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
// Turns off ESLint rules that overlap with (and would fight) Prettier formatting.
const prettier = require('eslint-config-prettier/flat');

module.exports = defineConfig([
  {
    // The OpenAPI TypeScript client is generated at build time (gitignored);
    // its style is the generator's concern, not ours. dist/coverage are build output.
    ignores: ['src/generated/**', 'dist/**', 'coverage/**'],
  },
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
      prettier,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
  {
    // Test doubles legitimately use empty handlers/mocks and signature-only
    // parameters; relax those two rules for specs only, keeping production
    // code strict. `args: 'none'` still flags genuinely unused local variables.
    files: ['**/*.spec.ts'],
    rules: {
      '@typescript-eslint/no-empty-function': 'off',
      '@typescript-eslint/no-unused-vars': ['error', { args: 'none' }],
    },
  },
]);
