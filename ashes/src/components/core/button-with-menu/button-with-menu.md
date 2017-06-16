#### Basic usage

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
  <ButtonWithMenu title="Save" items={items} />
  <ButtonWithMenu title="Save" items={items} buttonDisabled />
  <ButtonWithMenu title="Save" items={items} menuDisabled />
  <ButtonWithMenu title="Save" items={items} isLoading /><br />
  <ButtonWithMenu icon="edit" items={items} />
  <ButtonWithMenu title="Edit" items={items} icon="edit" />
  <ButtonWithMenu title="Edit" items={items} icon="edit" />
</div>
```
