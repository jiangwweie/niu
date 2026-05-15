/**
 * Download utilities
 */

export function parseFilenameFromContentDisposition(header: string | undefined, defaultFilename: string): string {
  if (!header) return defaultFilename;
  const encodedMatch = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (encodedMatch && encodedMatch[1]) {
    try {
      return decodeURIComponent(encodedMatch[1].replace(/^"|"$/g, ''));
    } catch {
      return encodedMatch[1].replace(/^"|"$/g, '');
    }
  }

  const match = header.match(/filename="?([^";]+)"?/i);
  if (match && match[1]) {
    // decode URI component if backend encoded it
    try {
      return decodeURIComponent(match[1]);
    } catch {
      return match[1];
    }
  }
  return defaultFilename;
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.style.display = 'none';
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
