#### Basic usage

```javascript
import { TextDropdown } from 'components/core/dropdown';

const items = [
  { value: 'One' },
  { value: 'Two' },
  { value: 'Three', displayText: 'Three!' }
];

<TextDropdown items={items} />
```

```
const items = [{ value: 'One' }, { value: 'Two' }, { value: 'Three', displayText: 'Three!' }];

<div style={{ display: 'flex' }}>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <TextDropdown items={items} onChange={e => console.log(e)} value="One" />
  </div>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <TextDropdown items={[]} disabled placeholder="disabled" />
  </div>
  <div style={{ width: '200px', marginRight: '10px' }}>
    <TextDropdown items={['One', 'Two', 'Three!']} stateless placeholder="Action" />
  </div>
  <TextDropdown placeholder="Empty" />
</div>
```
