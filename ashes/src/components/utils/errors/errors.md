#### Basic usage

```javascript
import Errors from 'components/utils/errors';

<Errors
  errors={['not found']}
  closeAction={handleClose}
  sanitizeError={preProcessError}
  className={s.customErrorsClassName}
/>
```

### States

```

class ErrorsExample extends React.Component {
  constructor() {
    this.state = {
      errorsVisible: true,
    };
  }

  handleClose() {
    this.setState({ errorsVisible: false });
  }

  render() {
    return (
      <div className="demo-blocked">
        <Errors
          errors={['Bad gateway', 'Good gateway', 'not found', 'not found']}
          sanitizeError={error => error + '!'}
        />

        {this.state.errorsVisible &&
          <Errors errors={['not found']} closeAction={this.handleClose.bind(this)} />
        }
      </div>
    );
  }
}

<ErrorsExample />
```
