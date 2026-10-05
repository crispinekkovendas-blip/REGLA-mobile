// Monorepo-aware Metro config. The workspace root hoists a different React
// version than this app pins, so force every `react` import (including the
// ones made from react-native inside the root node_modules) to this app's copy.
const path = require('path');
const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);
const appEntry = path.join(__dirname, 'package.json');

const upstream = config.resolver.resolveRequest;
config.resolver.resolveRequest = (context, moduleName, platform) => {
  const resolve = upstream ?? context.resolveRequest;
  if (moduleName === 'react' || moduleName.startsWith('react/')) {
    return resolve({ ...context, originModulePath: appEntry }, moduleName, platform);
  }
  return resolve(context, moduleName, platform);
};

module.exports = config;
