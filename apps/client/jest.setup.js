/* eslint-disable no-undef */
// AsyncStorage ships an official jest mock.
jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

// Never hit the network from tests.
global.fetch = jest.fn(() => Promise.reject(new Error('network disabled in tests')));
