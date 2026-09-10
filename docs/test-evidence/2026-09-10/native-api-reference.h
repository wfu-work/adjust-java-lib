#ifndef NAV_ADJUST_API_H
#define NAV_ADJUST_API_H

/*
 * Stable C ABI for nav-adjust-go-lib.
 *
 * JSON arguments and results are UTF-8, NUL-terminated strings. Every string
 * returned by this API is allocated by the library and must be released with
 * adjustFree. Input strings are borrowed for the duration of the call.
 */

#ifdef __cplusplus
extern "C" {
#endif

char *getVersion(void);
char *startAdjust(char *request_json);
char *startAdjustWgs84(char *request_json);
char *adjustValidate(char *request_json);
void adjustFree(char *value);

#ifdef __cplusplus
}
#endif

#endif /* NAV_ADJUST_API_H */
