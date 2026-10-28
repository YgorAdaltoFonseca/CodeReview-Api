# CodeReview API web client

React and TypeScript single-page application for the CodeReview API.

## Local development

Install Node.js 22.12 or newer and pnpm 11. Start the API on port `8080`, then run:

```bash
pnpm install --frozen-lockfile
pnpm dev
```

Vite serves the application on port `5173` and forwards `/api` requests to the local backend.

## Production build

```bash
pnpm install --frozen-lockfile
pnpm build
pnpm lint
```

The Docker image serves the built client with Nginx on port `80` and forwards API requests to the `api` Compose service.

## Authentication storage

The JWT remains in JavaScript memory and is cleared on reload or sign out. This avoids persistent browser storage and cookie-based CSRF exposure. A page reload requires a new login. The browser uses token claims only for display and navigation; the API enforces every role and ownership rule.
