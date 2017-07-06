##### Basic usage

```javascript
<NotificationItem displayed count={2} />
```

### States

```javascript
import Item from 'components/activity-notifications/item.jsx'
import Panel from 'components/activity-notifications/panel.jsx'
```

```
const userItem = { data: { user: {} }, kind: 'user_created', createdAt: 'Mon Jun 05 2017' };
const cardItem = { data: { context: {} }, kind: 'credit_card_added', createdAt: 'Mon Jun 05 2017' };

<div className="demo">
  <NotificationItem item={{ data: {} }} />
  <NotificationItem item={userItem} />
  <NotificationItem item={cardItem} />
</div>
```
