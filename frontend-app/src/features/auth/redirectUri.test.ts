import { currentApplicationUrl } from './redirectUri'

describe('currentApplicationUrl', () => {
  it('keeps the protected route and query but excludes OAuth hash parameters', () => {
    const location = {
      origin: 'http://localhost:5173',
      pathname: '/files',
      search: '?folder=work',
      hash: '#code=oauth-code&state=state-value',
    }

    expect(currentApplicationUrl(location)).toBe('http://localhost:5173/files?folder=work')
  })
})
