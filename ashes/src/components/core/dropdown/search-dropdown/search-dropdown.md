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

<div style={{ display: 'flex' }}>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <SearchDropdown items={items} onChange={e => console.log(e)} value="One" />
  </div>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <SearchDropdown items={[]} disabled placeholder="disabled" />
  </div>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <SearchDropdown items={['One', 'Two', 'Three!']} stateless placeholder="Action" />
  </div>
  <SearchDropdown placeholder="Empty" />
</div>
```
