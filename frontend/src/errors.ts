import { ApiError } from './api/client';
import { t } from './i18n/sr';

/** A user-facing Serbian message for any error thrown by an API call. */
export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    return (err.code && t.errors.codes[err.code]) || t.errors.generic;
  }
  if (err instanceof TypeError) return t.errors.network;
  return t.errors.generic;
}

/** Serbian text for a backend field validation message. */
export function fieldMessage(message: string | undefined): string | undefined {
  if (!message) return undefined;
  return t.errors.fields[message] ?? t.errors.invalidValue;
}

/** Field errors of an API error, translated, keyed like the backend ("customer.name"). */
export function fieldErrors(err: unknown): Record<string, string> {
  if (!(err instanceof ApiError)) return {};
  return Object.fromEntries(Object.entries(err.fieldErrors ?? {}).map(([k, v]) => [k, fieldMessage(v)!]));
}
