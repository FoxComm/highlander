##### Basic usage

```javascript
<SaveCancel
  className={styles.button}
  cancelLabel="Back"
  saveLabel="Ok"
  onSave={saveHandler}
  onCancel={cancelHandler}
  isLoading={saveState.inProgress}
  saveDisabled={!saveState.finished}
  cancelDisabled={!saveState.finished}
/>
```

### Simple SaveCancel Buttons

```javascript
import { SaveCancel } from 'components/core/save-cancel'
```

```
<SaveCancel saveLabel="Ok" cancelLabel="Back" />
```

### States

```
<div className="demo">
  <SaveCancel />
  <SaveCancel isLoading />
  <SaveCancel saveDisabled />
  <SaveCancel cancelDisabled />
  <SaveCancel saveDisabled cancelDisabled />
</div>
```

### Save with menu

```javascript
import { SaveCancel } from 'components/core/save-cancel'
```

```
<SaveCancel
  saveItems={[
      ['id1', 'Save and Exit'],
      ['id2', 'Save and Duplicate'],
  ]}
/>
```
