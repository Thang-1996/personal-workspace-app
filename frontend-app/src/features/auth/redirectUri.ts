export function currentApplicationUrl(location: Pick<Location, 'origin' | 'pathname' | 'search'> = window.location) {
  return `${location.origin}${location.pathname}${location.search}`
}
