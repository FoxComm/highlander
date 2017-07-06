##### Basic usage

```javascript
import { ButtonWithMenu } from 'components/core/button-with-menu';

<ButtonWithMenu
  className={styles.button}
  icon="save"
  title="Save"

  items={[
      ['id1', 'Save and Exit'],
      ['id2', 'Save and Duplicate'],
  ]}
  onPrimaryClick={saveHandler}
  onSelect={menuSelectHandler}
  isLoading={saveState.inProgress}
  buttonDisabled={!saveState.finished}
  menuDisabled={!saveState.finished}
/>
```

### States

```
const items=[['id1', 'Save and Exit'], ['id2', 'Save and Duplicate']];

<div className="demo">
  <div className="demo-inline">
    <ButtonWithMenu title="Save" items={items} />
    <ButtonWithMenu title="Save" items={items} buttonDisabled />
    <ButtonWithMenu title="Save" items={items} menuDisabled />
    <ButtonWithMenu title="Save" items={items} isLoading /><br />
  </div>

  <div className="demo-inline">
    <ButtonWithMenu title="Edit" items={items} icon="edit" />
    <ButtonWithMenu icon="edit" items={items} />
  </div>
</div>
```
