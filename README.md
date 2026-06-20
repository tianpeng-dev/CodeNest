# CodeNest

CodeNest is a technical blog and forum platform with a Vue 3 frontend and a Spring Boot 3 backend.

## Tech Stack

- Vue 3
- TypeScript
- Vite
- Element Plus
- Vue Router
- Pinia
- Axios
- Spring Boot 3
- MyBatis-Plus
- MySQL 8.0
- Clerk

## Development

```bash
npm install
npm run dev
```

Frontend integration settings:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_CLERK_PUBLISHABLE_KEY=pk_test_placeholder
```

Set `VITE_USE_MOCK_API=true` only when you want the frontend to use the local Axios mock data instead of the Spring Boot API.

Backend integration settings:

```bash
CODENEST_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-site.netlify.app
CLERK_ISSUER=https://your-clerk-instance.clerk.accounts.dev
CLERK_JWKS_URI=https://your-clerk-instance.clerk.accounts.dev/.well-known/jwks.json
```

## Verification

```bash
npm run build
npm run test:unit
npm run test:e2e

cd backend
mvn test
```

## Test Accounts

- Normal user: `writer` / `password123`
- Admin: `admin` / `admin123`

## Routes and Features

- Public: home, search, category pages, post detail pages, and user profiles.
- Authenticated user: creator center, editor, content management, notifications, and messages.
- Admin: dashboard, users, posts, categories, moderators, sensitive words, and analytics.

## Backend Scope

The backend includes Clerk JWT authentication, user sync, categories, posts, comments, sensitive-word moderation, follows, notifications, messages, uploads, admin APIs, moderator-scoped review, analytics, Flyway migrations, and OpenAPI docs.

## Deployment Notes

Netlify SPA fallback is configured in `netlify.toml` so direct route refreshes serve `index.html`.
