#### Basic usage

```javascript
<RoundedPill
  className={styles.button}
  icon="fetch"
  onClick={handler}
  isLoading={fetchState.inProgress}
  disabled={!fetchState.finished}
>
  Fetch Items
</RoundedPill>
```
### States

```
<div className="demo">
  <RoundedPill.RoundedPill text="Ready" />
  <RoundedPill.RoundedPill text="Clickable" onClick={() => alert('clicked')} />
  <RoundedPill.RoundedPill text="" onClose={() => {}} value={1} />
  <RoundedPill.RoundedPill text="Clickable X" onClick={() => alert('clicked')} onClose={() => {}} value={1} />
  <RoundedPill.RoundedPill text="Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum." onClose={() => {}} value={1} />
</div>
```
