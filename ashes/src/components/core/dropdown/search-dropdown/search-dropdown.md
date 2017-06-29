#### Basic usage

```javascript
import { SearchDropdown } from 'components/core/dropdown';

function fetch(token) {
  return new Promise(...);
};

<SearchDropdown fetch={(token) => new Promise(...)} />
```

```
function fetch(token) {
  return new Promise(function(resolve, reject) {
    const items = _.uniq(token.split('')).map(ch => ch);

    setTimeout(() => resolve({ items, token }), 1000);
  });
};

<div style={{ display: 'flex' }}>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <SearchDropdown fetch={fetch} initialItems={['Initial item 1']} />
  </div>
</div>
```
