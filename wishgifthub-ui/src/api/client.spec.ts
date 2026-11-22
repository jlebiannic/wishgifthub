import {afterEach, expect, it, vi} from 'vitest'
import {initApiClient} from './client'

afterEach(() => vi.restoreAllMocks())

it('keeps generated API paths under the deployment base', async () => {
  const api = initApiClient()
  let requestUrl = ''
  api.instance.defaults.adapter = async config => {
    requestUrl = api.instance.getUri(config)
    return {data: {}, status: 200, statusText: 'OK', headers: {}, config}
  }
  await api.request({path: '/api/version', method: 'GET'})
  expect(requestUrl).toBe(`${import.meta.env.BASE_URL}api/version`)
})
