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
  <RoundedPill.RoundedPill text="Loading" inProgress onClose={() => {}} value={1} />
  <RoundedPill.RoundedPill text="Clickable" onClick={() => alert('clicked')} />
  <RoundedPill.RoundedPill text="" onClose={() => {}} value={1} />
  <RoundedPill.RoundedPill text="Clickable X" onClick={() => alert('clicked')} onClose={() => {}} value={1} />
  <RoundedPill.RoundedPill onClose={() => {}} value={1}>
    <Icon name="filter" /> filter
  </RoundedPill.RoundedPill>
</div>
```
