import {transformJsonToFormUrlEncoded} from 'services/Form';

export function ResetPass(data) {
  return {
    type: 'API_RESET',
    payload: {
      request: {
        transformRequest: transformJsonToFormUrlEncoded,
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        method: 'post',
        url: '/resetPass',
        data
      }
    }
  };
}
