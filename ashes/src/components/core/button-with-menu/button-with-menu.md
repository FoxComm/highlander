#### Basic usage

```javascript
<ButtonWithMenu
  className={styles.button}
  icon="save"
  title="Save"
  menuPosition="right"
  items={[
      ['id1', 'Save and Exit'],
      ['id2', 'Save and Duplicate'],
  ]}
  onPrimaryClick={saveHandler}
  onSelect={menuSelectHandler}
  isLoading={saveState.inProgress}
  buttonDisabled={!saveState.finished}
  menuDisabled={!saveState.finished}
  animate
/>
```

### ButtonWithMenu

```javascript
import { ButtonWithMenu } from 'components/core/button-with-menu'
```

```
<ButtonWithMenu
  title="Save"
  items={[
      ['id1', 'Save and Exit'],
      ['id2', 'Save and Duplicate'],
  ]}
/>
```

### States

```
const items=[['id1', 'Save and Exit'], ['id2', 'Save and Duplicate']];

<div className="demo">
  <ButtonWithMenu title="Save" items={items} menuPosition="right" />
  <ButtonWithMenu title="Save" items={items} menuPosition="right" buttonDisabled />
  <ButtonWithMenu title="Save" items={items} menuPosition="right" menuDisabled />
  <ButtonWithMenu title="Save" items={items} menuPosition="right" isLoading /><br />
  <ButtonWithMenu icon="edit" items={items} menuPosition="right" />
  <ButtonWithMenu title="Edit" items={items} menuPosition="right" icon="edit" />
  <ButtonWithMenu title="Edit" items={items} menuPosition="right" icon="edit" animate={false} />
</div>
```
