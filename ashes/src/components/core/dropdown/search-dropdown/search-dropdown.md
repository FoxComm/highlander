#### Basic usage

```javascript
import { SearchDropdown } from 'components/core/dropdown';

const items = [
  { value: 'One' },
  { value: 'Two' },
  { value: 'Three', displayText: 'Three!' }
];

<SearchDropdown items={items} />
```

```
const items = [{ value: 'One' }, { value: 'Two' }, { value: 'Three', displayText: 'Three!' }];

function fetch(token) {
  return new Promise(function(resolve, reject) {
    setTimeout(() => resolve({ items, token }), 1000);
  });
};

<div style={{ display: 'flex' }}>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <SearchDropdown fetch={fetch} />
  </div>
</div>
```
