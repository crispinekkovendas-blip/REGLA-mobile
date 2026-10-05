/* eslint-env jest */
jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

// Never talk to a real Supabase in tests — screens/components get a stub client.
jest.mock('./src/lib/supabase', () => {
  const sb = { __fake: true };
  return {
    SUPABASE_URL: '',
    isConfigured: true,
    supabase: sb,
    getSupabase: () => sb,
  };
});
