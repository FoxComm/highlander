import _ from 'lodash';
import { createMatchMediaConnect } from 'react-matchmedia-connect';

export const responsiveConnect = createMatchMediaConnect({
  isMedium: '(min-width: 768px)',
  isSmallOnly: '(max-width: 767px)',
}, {
  matchMediaFn: (query) => {
    if (typeof window != 'undefined') {
      return window.matchMedia(query);
    }
    return {
      matches: false,
      addListener: _.noop,
      removeListener: _.noop,
    };
  }
});

export default responsiveConnect(['isSmallOnly', 'isMedium']);
