import js from '@eslint/js';
import globals from 'globals';
import reactPlugin from 'eslint-plugin-react';
import reactHooksPlugin from 'eslint-plugin-react-hooks';
import reactRefreshPlugin from 'eslint-plugin-react-refresh';
import prettierPlugin from 'eslint-plugin-prettier';
import prettierConfig from 'eslint-config-prettier';

/**
 * ESLint flat-config for the Employee Management Portal frontend.
 * Compatible with ESLint 9.x flat-config format.
 */
export default [
  { ignores: ['dist/**', 'node_modules/**'] },

  js.configs.recommended,

  {
    files: ['src/**/*.{js,jsx}'],
    plugins: {
      react:          reactPlugin,
      'react-hooks':  reactHooksPlugin,
      'react-refresh': reactRefreshPlugin,
      prettier:       prettierPlugin,
    },
    languageOptions: {
      ecmaVersion: 2024,
      sourceType:  'module',
      globals: {
        ...globals.browser,
        ...globals.es2024,
      },
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    settings: {
      react: { version: 'detect' },
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      ...reactHooksPlugin.configs.recommended.rules,
      ...prettierConfig.rules,
      'prettier/prettier':              'error',
      'react/react-in-jsx-scope':       'off',       // Not needed with React 19 JSX transform
      'react/prop-types':               'off',       // Using JSDoc instead
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'no-unused-vars':                 ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-console':                     ['warn', { allow: ['warn', 'error'] }],
      'prefer-const':                   'error',
      'no-var':                         'error',
    },
  },

  // ── Test-file overrides ────────────────────────────────────────────────────
  // Must come AFTER the main block so these rules take precedence.
  // Test files run in a jsdom/vitest environment that exposes Node globals
  // such as `global`, and commonly use intentional empty catch blocks and
  // anonymous wrapper components.
  {
    files: ['src/tests/**/*.{js,jsx}'],
    plugins: {
      react: reactPlugin,
    },
    languageOptions: {
      ecmaVersion: 2024,
      sourceType:  'module',
      globals: {
        ...globals.browser,
        ...globals.es2024,
        ...globals.node,
      },
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    rules: {
      'no-empty':                         'off',
      'react/display-name':               'off',
      // Test files intentionally import testing utilities that may not all be
      // used in every suite; suppress unused-var noise rather than cluttering
      // each test file with eslint-disable comments.
      'no-unused-vars':                   'off',
    },
  },
];
