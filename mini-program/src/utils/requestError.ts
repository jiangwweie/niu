function isUsefulMessage(message?: string): message is string {
  if (!message) return false;
  return !message.includes('request:fail');
}

export function getRequestErrorMessage(error: unknown, fallback: string): string {
  const message = error instanceof Error
    ? error.message
    : typeof error === 'object' && error !== null && 'message' in error
      ? String((error as { message?: unknown }).message || '')
      : '';

  return isUsefulMessage(message) ? message : fallback;
}

export function showRequestErrorToast(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'feedbackShown' in error
      && (error as { feedbackShown?: boolean }).feedbackShown) {
    return;
  }

  wx.showToast({
    title: getRequestErrorMessage(error, fallback),
    icon: 'none',
    duration: 2500
  });
}
