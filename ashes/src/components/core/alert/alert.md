#### Basic usage

```javascript
import Alert from 'components/core/alert';

<Alert
  type={Alert.SUCCESS}
  closeAction={handleClose}
/>
```

### States

```
const moment = require('moment');

class Alerts extends React.Component {
  constructor() {
    this.state = {
      successVisible: true,
      warningVisible: true,
      errorVisible: true,
    };
  }

  handleClose(type) {
    this.setState({ [`${type}Visible`]: false });
  }

  render() {
    return (
      <div className="demo">
        <Alert type={Alert.SUCCESS}>
          Fetched successfully
        </Alert>

        <Alert type={Alert.SUCCESS}>
          Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully.
          Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully.
          Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully.
        </Alert>

        <Alert type={Alert.SUCCESS} closeAction={() => ({})}>
          Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully.
          Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully.
          Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully. Fetched successfully.
        </Alert>

        {this.state.successVisible &&
          <Alert type={Alert.SUCCESS} closeAction={this.handleClose.bind(this, 'success')}>
            Fetched successfully
          </Alert>
        }

        {this.state.warningVisible &&
          <Alert type={Alert.WARNING} closeAction={this.handleClose.bind(this, 'warning')}>
            Fetched successfully
          </Alert>
        }

        {this.state.errorVisible &&
          <Alert type={Alert.ERROR} closeAction={this.handleClose.bind(this, 'error')}>
            Fetched successfully
          </Alert>
        }
      </div>
    );
  }
}

  <Alerts />
```
