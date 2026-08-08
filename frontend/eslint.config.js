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
      'no-unused-vars':                 ['warn', { argsIgnorePattern: '^_' }],
      'no-console':                     ['warn', { allow: ['warn', 'error'] }],
      'prefer-const':                   'error',
      'no-var':                         'error',
    },
  },
];
