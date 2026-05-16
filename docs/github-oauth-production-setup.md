# GitHub OAuth Production Setup

## 1. Overview

The platform integrates with GitHub via OAuth for repository access and Pull Request review. This is a **read-only** integration — the platform reads repositories, PRs, diffs, and patches but never pushes, merges, approves, or comments on PRs.

## 2. Creating a GitHub OAuth App

### Step 1: Navigate to Developer Settings

1. Go to [GitHub Settings → Developer settings → OAuth Apps](https://github.com/settings/developers).
2. Click **New OAuth App**.

### Step 2: Fill in Application Details

| Field | Value |
|-------|-------|
| Application name | `AI Coding Platform` (or your choice) |
| Homepage URL | `https://example.com` (your production domain) |
| Application description | `Enterprise AI Coding Collaboration Platform` |
| Authorization callback URL | `https://example.com/api/github/oauth/callback` |

For **local development**, the callback URL would be:
```
http://localhost:8080/api/github/oauth/callback
```

### Step 3: Register and Get Credentials

1. Click **Register application**.
2. Click **Generate a new client secret**.
3. Copy the **Client ID** and **Client Secret** immediately.

## 3. Required Scopes

The platform requests these OAuth scopes:

| Scope | Purpose | Risk |
|-------|---------|------|
| `read:user` | Read GitHub user profile | Minimal |
| `user:email` | Read user email addresses | Minimal |
| `repo` | Access public and private repositories | **High** — grants full read access to all repos |

### Reducing Scope Risk

If you only need public repository access, change scopes to:
```
read:user,user:email,public_repo
```

Or if you only need specific org repos, create a GitHub App instead of OAuth App for finer-grained permissions.

## 4. Environment Variables

Add to `.env.production`:

```bash
# ---- GitHub OAuth ----
GITHUB_CLIENT_ID=your-client-id
GITHUB_CLIENT_SECRET=your-client-secret
GITHUB_REDIRECT_URI=https://example.com/api/github/oauth/callback
GITHUB_OAUTH_SCOPES=read:user,user:email,repo
```

### Variable Reference

| Variable | Required | Default | Notes |
|----------|----------|---------|-------|
| `GITHUB_CLIENT_ID` | Yes (for OAuth) | (empty) | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | Yes (for OAuth) | (empty) | GitHub OAuth App Client Secret |
| `GITHUB_REDIRECT_URI` | Yes (for OAuth) | `http://localhost:8080/api/github/oauth/callback` | Must match the OAuth App setting |
| `GITHUB_OAUTH_SCOPES` | No | `repo,read:user,user:email` | Space for comma-separated scopes |

## 5. Authorization Flow

```
User → Frontend → Backend POST /api/github/oauth/authorize
  → Backend generates state, returns authorize URL
  → User clicks link, browser redirects to github.com/login/oauth/authorize
  → User authorizes, GitHub redirects to /api/github/oauth/callback?code=...&state=...
  → Backend exchanges code for token, stores token, shows success page
  → Frontend polls /api/github/oauth/status → shows "Bound"
```

## 6. Verifying Configuration

### Check if configured

```bash
bash scripts/validate-github-oauth-config.sh
```

### Test the authorization endpoint

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123456"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# Get authorization URL
curl -s http://localhost:8080/api/github/oauth/authorize \
  -H "Authorization: Bearer $TOKEN"
```

### Response when NOT configured

```json
{
  "data": {
    "configured": false
  }
}
```

### Response when configured

```json
{
  "data": {
    "configured": true,
    "authorizeUrl": "https://github.com/login/oauth/authorize?client_id=...",
    "state": "..."
  }
}
```

## 7. Binding Status

After successful authorization:

```bash
curl -s http://localhost:8080/api/github/oauth/status \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "data": {
    "configured": true,
    "bound": true,
    "githubLogin": "your-github-username",
    "githubUserId": 123456
  }
}
```

## 8. Repository Sync

After binding, sync repositories from the GitHub Integration page or via API:

```bash
curl -s -X POST http://localhost:8080/api/github/repositories/sync \
  -H "Authorization: Bearer $TOKEN"
```

This fetches your accessible repositories without storing their contents locally — only metadata (name, full name, description, default branch, stars, language) is persisted.

## 9. PR Review (Read-Only)

The PR Review feature:
- Lists open PRs for a selected repository.
- Fetches PR details, changed files, and diffs.
- Sends the diff to the configured AI model for review.
- Displays findings (severity, category, file, line, description, suggestion).

**The integration NEVER:**
- Pushes code
- Creates/merges/closes PRs
- Approves or requests changes
- Comments on PRs
- Modifies any repository data

## 10. Security

### Token Storage
- GitHub OAuth access tokens are stored **encrypted** in the database (`accessTokenEnc` field).
- Tokens are **never** returned in API responses.
- The `/api/github/oauth/status` endpoint only returns `githubLogin` and `githubUserId` — not the token.

### Token in Logs
- GitHub token is **never** logged.
- Token exchange HTTP calls use `log.error()` only, which includes HTTP status codes but not the token body.

### Token in Prompts
- PR Review prompts contain only PR metadata (title, author, branch names, file list, diff/patch).
- **No GitHub OAuth token, API key, or client secret enters the AI prompt.**

## 11. Unbinding

To revoke access:

```bash
# Frontend: GitHub Integration → Unbind
# Or API:
curl -s -X DELETE http://localhost:8080/api/github/oauth/bindings/{bindingId} \
  -H "Authorization: Bearer $TOKEN"
```

The token is cleared from the database and the status is set to `REVOKED`.

## 12. Troubleshooting

| Issue | Possible Cause | Fix |
|-------|---------------|-----|
| "Not configured" in UI | `GITHUB_CLIENT_ID` or `GITHUB_CLIENT_SECRET` not set | Set both in `.env.production` |
| "redirect_uri mismatch" | Callback URL doesn't match OAuth App setting | Update `GITHUB_REDIRECT_URI` or OAuth App callback URL |
| "bad_verification_code" | OAuth code expired or reused | Restart authorization flow |
| "Token exchange failed" | Client secret wrong | Verify `GITHUB_CLIENT_SECRET` in `.env.production` |
| "Token missing" when accessing repos | User hasn't bound GitHub | Go through OAuth flow first |
| Repository list empty | No repos accessible with current token scope | Check token has `repo` scope |
