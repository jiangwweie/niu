export function normalizeSearchParam(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const trimmed = value.trim();
  return trimmed ? trimmed : undefined;
}

export function setSearchParam<T extends object>(params: T, key: string, value: unknown) {
  const normalized = normalizeSearchParam(value);
  if (normalized !== undefined) {
    (params as Record<string, unknown>)[key] = normalized;
  }
}

export function trimSearchFields<T extends object>(target: T, fields: string[]) {
  const record = target as Record<string, unknown>;
  fields.forEach((field) => {
    const value = record[field];
    if (typeof value === 'string') {
      record[field] = value.trim();
    }
  });
}
