/**
 * Format a datetime string for display.
 * Handles both ISO format (2026-05-09T09:00:00) and standard format (2026-05-09 09:00:00).
 * Returns 'YYYY-MM-DD HH:mm' format.
 * Empty/null/undefined values return '-'.
 * Invalid dates return the original string to avoid showing 'Invalid Date'.
 */
export function formatDateTime(dt?: string | null): string {
  if (!dt) return '-';
  // Replace T with space and take first 19 chars (YYYY-MM-DD HH:mm:ss)
  const cleaned = dt.replace('T', ' ');
  if (cleaned.length >= 19) {
    return cleaned.substring(0, 19);
  } else if (cleaned.length >= 16) {
    // If backend returns only YYYY-MM-DD HH:mm, pad with :00
    return cleaned.substring(0, 16) + ':00';
  }
  // Short string (date only or partial) — return as-is
  return cleaned;
}

/**
 * Format a date-only string for display.
 * Returns 'YYYY-MM-DD' format.
 * Empty/null/undefined values return '-'.
 */
export function formatDate(dt?: string | null): string {
  if (!dt) return '-';
  const cleaned = dt.replace('T', ' ');
  if (cleaned.length >= 10) {
    return cleaned.substring(0, 10);
  }
  return cleaned;
}
