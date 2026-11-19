/**
 * Safely extracts array items from either a Spring Data Page response
 * ({ content: [...], totalElements: ... }) or a plain Array response.
 */
export function extractPageContent(data) {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.content)) return data.content;
  return [];
}

/**
 * Extracts a readable error string from Axios/Spring Boot error responses,
 * including Spring's MethodArgumentNotValidException field error maps.
 */
export function formatApiErrorMessage(err, fallback = 'Operation failed') {
  if (!err) return fallback;
  const data = err.response?.data;
  if (!data) return err.message || fallback;

  if (typeof data === 'string') return data;
  if (data.message) return data.message;
  if (data.error) return data.error;

  // Spring validation errors map: { fieldName: errorMessage, ... }
  if (typeof data === 'object') {
    const errorValues = Object.values(data).filter((v) => typeof v === 'string');
    if (errorValues.length > 0) {
      return errorValues.join('. ');
    }
  }

  return fallback;
}
