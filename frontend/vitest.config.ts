import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    // Dates in tests are compared in a fixed browser time zone.
    env: { TZ: 'Europe/Belgrade' },
  },
});
