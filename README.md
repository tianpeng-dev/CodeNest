# CodeNest

CodeNest is a Vue 3 frontend prototype for a technical blog and forum platform.

## Tech Stack

- Vue 3
- TypeScript
- Vite
- Element Plus
- Vue Router
- Pinia
- Axios
- Mock API

## Development

```bash
npm install
npm run dev
```

## Verification

```bash
npm run build
npm run test:unit
npm run test:e2e
```

## Test Accounts

- Normal user: `writer` / `password123`
- Admin: `admin` / `admin123`

## Routes and Features

- Public: home, search, category pages, post detail pages, and user profiles.
- Authenticated user: creator center, editor, content management, notifications, and messages.
- Admin: dashboard, users, posts, categories, moderators, sensitive words, and analytics.

## Scope

V1 is frontend-only. Backend integration, real-time messaging, real AI generation, and deep moderation workflows are outside V1.

## Deployment Notes

Netlify SPA fallback is configured in `netlify.toml` so direct route refreshes serve `index.html`.
