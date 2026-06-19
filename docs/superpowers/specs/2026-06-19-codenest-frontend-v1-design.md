# CodeNest Frontend V1 Design

Date: 2026-06-19

## Goal

CodeNest V1 is a frontend-first technical blog and forum prototype inspired by CSDN. It should demonstrate the complete product surface for normal users and administrators while keeping implementation depth controlled.

The first version uses Vue 3, Element Plus, Vue Router, Axios, Pinia, and Mock API data. It should be deployable as a frontend-only app on Netlify and later connect to a Spring Boot 3, MyBatis-Plus, JWT, and MySQL backend with minimal changes to page-level code.

## Confirmed Decisions

- V1 scope: full-module frontend prototype covering public site, creator center, and admin console.
- Data realism: Mock API plus local state loops, not static-only pages.
- Admin depth: dashboard and list display first, with visible operation entrances but no deep moderation workflow.
- AI creation: entry placeholders only for AI topic selection and AI article generation.
- Auth: self-built JWT-style login and registration flow with mock endpoints.
- Editor: Markdown is the source format; rich-text mode is auxiliary light editing or preview.
- Responsive priority: desktop first, mobile must remain readable and usable at a basic level.

## V1 Scope

### Public Site

The public site includes:

- Home page with left sidebar, top navigation, search, creator button, category filters, headline news, carousel, recommended blog list, community recommendations, event calendar, friend links, and filing information.
- Search and category result pages.
- Blog post detail page with author sidebar, post content, tags, publish time, counts, interaction actions, comments, sharing, and report entrance.
- User profile page.
- Notifications page.
- Messages page.

Public interactions in V1 include like, dislike, favorite, follow, comment, and report entrance. These interactions update local or mock state.

### Creator Center

The creator center includes:

- Creator overview.
- Post creation entrance.
- AI topic and AI article generation placeholder entrances.
- Dual-mode editor page.
- Content management.
- Comment management.
- Column management.
- Creator analytics with charts and data lists.

The analytics area should include at least a trend line chart, a content type pie chart, and an article data table.

### Admin Console

The admin console includes:

- Admin dashboard.
- User management list.
- Blog management list.
- Category management list.
- Section moderator management list.
- Sensitive word management list.
- Admin analytics page.

V1 admin pages are display-first. Buttons such as ban, mute, hide post, edit, and delete may show confirmation dialogs, success messages, and local state changes, but they do not implement a full audit, workflow, log, or permission delegation system.

## Explicitly Out Of Scope For V1

- Real backend integration.
- Real-time notifications.
- Real-time private messaging.
- Full private message conversation system.
- Real AI generation.
- Complex admin audit workflow.
- Operation logs.
- Batch export.
- Advanced role and section-level permission delegation.
- Full mobile experience.
- Server-side rendering or full SEO optimization.
- Real file or object storage.

These can be revisited in V1.1 or V2.

## Information Architecture And Routing

Use three application layouts plus standalone auth pages.

### PublicLayout

Purpose: visitor and normal user content consumption.

Routes:

- `/`
- `/search`
- `/category/:slug`
- `/post/:id`
- `/u/:id`
- `/notifications`
- `/messages`

The layout contains top navigation, collapsible menu control, search, login/register entry, creator button, and public content structure.

### UserCenterLayout

Purpose: authenticated creator and personal management area.

Routes:

- `/creator/overview`
- `/creator/editor`
- `/creator/posts`
- `/creator/comments`
- `/creator/columns`
- `/creator/analytics`

Access requires login.

### AdminLayout

Purpose: administrator management area.

Routes:

- `/admin`
- `/admin/users`
- `/admin/posts`
- `/admin/categories`
- `/admin/moderators`
- `/admin/sensitive-words`
- `/admin/analytics`

Access requires an authenticated user with the `admin` role.

### Auth And Error Routes

Routes:

- `/login`
- `/register`
- `/403`
- `/404`

Auth redirects should preserve the intended destination with a `redirect` query parameter.

## Frontend Architecture

Recommended source structure:

```text
src/
  app/
  layouts/
  pages/
    public/
    auth/
    creator/
    admin/
    errors/
  modules/
    post/
    user/
    comment/
    category/
    notification/
    message/
    admin/
    editor/
    analytics/
  services/
    auth.service.ts
    post.service.ts
    user.service.ts
    admin.service.ts
  mocks/
  stores/
  components/
  utils/
  styles/
  types/
```

`pages` compose route-level screens and should not hold complex business logic.

`modules` contain reusable domain components such as `PostCard`, `AuthorSidebar`, `CommentList`, `InteractionBar`, `EditorShell`, and `AdminDataTable`.

`services` define the stable interface between UI code and backend-like data operations. V1 services call mock endpoints through Axios. Later backend integration should mainly replace mock handlers, base URLs, token handling, and response mapping.

`stores` hold cross-page state such as authentication, current user, interactions, drafts, and lightweight UI state. They should not become the default home for all list data.

## Data Flow

Use a simple unidirectional flow:

1. A page loads or a user performs an action.
2. The page or store calls a service method.
3. The service uses Axios.
4. Mock API returns backend-like data.
5. Page local state or Pinia state updates.
6. UI re-renders.

Pages and stores should not import mock data directly.

## Mock API Coverage

### Auth

- Register.
- Login.
- Logout.
- Get current user.
- Simulate JWT token expiration.

### Posts

- Home recommendations.
- Category list.
- Search results.
- Post detail.
- Create draft.
- Publish post.
- Update post.
- Delete post.
- Like.
- Dislike.
- Favorite.
- Report entrance.

### Comments

- Get comment list.
- Create comment.
- Delete own comment.

Nested comments are out of scope for V1.

### Users

- User profile.
- Follow and unfollow.
- Creator profile summary.

### Creator

- My posts.
- Comment management.
- Column management.
- Creator analytics.

### Admin

- User list.
- Blog list.
- Category list.
- Section moderator list.
- Sensitive word list.
- Analytics overview.

Admin mutations may return success and update local mock state, but no deep moderation workflow is required.

## State Management

Pinia stores:

- `authStore`: token, current user, role, login state, permission helpers.
- `interactionStore`: per-post like, favorite, dislike, and follow states for the current user.
- `draftStore`: editor draft, active editing mode, tags, cover, summary, publish state.
- `uiStore`: sidebar collapse, theme, and lightweight global loading state.

List data can live in page state unless it is genuinely shared across unrelated routes.

## Error Handling

- Unauthenticated access redirects to `/login?redirect=<target>`.
- Unauthorized admin access redirects to `/403`.
- Mock API failures show Element Plus message feedback and page-level empty or error states.
- Form validation should use field-level feedback.
- Mock examples should include duplicate username, invalid credentials, token expiration, and sensitive-word validation.
- Empty lists should use designed empty states instead of blank tables.

## Editor Design

Markdown is the canonical storage format.

V1 editor capabilities:

- Markdown editing.
- Preview mode.
- Code block rendering.
- Category selection.
- Tag selection.
- Cover image mock selection or local preview.
- Summary field.
- Save draft.
- Publish.
- Rich-text light editing or preview mode.

The rich-text mode is not required to support full bidirectional equivalence with Markdown. Complex formatting parity is out of scope.

## Page Depth

### Home

The home page includes:

- Left sidebar with menu, friend links, and filing information.
- Top area with menu collapse, search, login/register, and creator button.
- Center area with technical category filters, headline news, carousel, and recommended blogs.
- Right area with community recommendations and event calendar.

Recommended blog loading should use pagination in V1.

### Post Detail

The detail page includes:

- Left author information sidebar.
- Author post count, likes, favorites, followers.
- Follow and private message entrances.
- Hot articles.
- Latest comments.
- Center post title, publish time, tags, counts, and body.
- Bottom author block and interaction bar.

### Creator Center

The creator center includes all confirmed creator pages. AI entrances open placeholder dialogs explaining that integration will come later.

### Messages And Notifications

Messages include a conversation list, read-only detail area, and disabled or mock-success send action.

Notifications include a notification list and local read state.

No real-time behavior is required.

### Admin

Admin pages use a management-console layout with tables, filters, statistic cards, and charts. V1 keeps data governance shallow and visible.

## Domain Model Notes

Even in V1 mock data, use stable status fields:

- Post status: `draft`, `published`, `hidden`, `deleted`.
- User role: `user`, `admin`.
- User status: `active`, `banned`.
- User mute field: `muteUntil`.
- Notification read field: `readAt`.
- Common timestamps: `createdAt`, `updatedAt`, and where useful `publishedAt`.

Separate categories from tags:

- Categories are site-managed navigation and filtering dimensions.
- Tags are author/content-level descriptors.

## Security And Safety Considerations

- Do not rely only on hidden menus for permissions. Use route guards, button visibility rules, and mock API permission failures.
- Markdown rendering should reserve a sanitize layer, such as DOMPurify, to prevent XSS once real user content exists.
- File upload is mocked in V1. Avatar, cover, and article images can use local preview URLs or preset images.

## SEO And Deployment

The V1 app is an SPA suitable for Netlify deployment.

Minimum V1 SEO work:

- Set page titles.
- Provide basic meta description handling.
- Add share link placeholders.

Full SEO, SSR, SSG, or pre-rendering is deferred. If the project becomes public-content heavy, Nuxt or pre-rendering should be reconsidered.

## Testing And Verification Strategy

V1 should verify:

- Route guards for guest, user, and admin states.
- Auth flow using mock JWT.
- Key public pages render with mock data.
- Creator editor can save draft and publish through mock service.
- Post interactions update UI state.
- Admin pages render lists, filters, and summary data.
- Empty and error states are visible.
- Desktop layout works well.
- Mobile layout remains readable and does not overlap.

Automated tests can start with service/store unit tests and a small set of route-level smoke tests.

## V1.1 And V2 Candidates

V1.1 candidates:

- Deeper admin actions.
- Real backend integration.
- Real upload flow.
- More complete mobile layouts.
- Search history and richer filters.
- Nested comments.

V2 candidates:

- Real-time notifications.
- Real private messaging.
- Real AI content generation.
- Full moderation workflows.
- Operation logs.
- Fine-grained admin roles.
- SSR, SSG, or pre-rendering for SEO.
