##### Basic usage

```javascript
import { ApiErrors } from 'components/utils/errors';

<ApiErrors
  response={state.asyncActions.fetchSmth.err}
  closeAction={handleClose}
  sanitizeError={preProcessError}
  className={s.customErrorsClassName}
/>
```

### States

```
const set = require('lodash/set');

const errorResponse = new Error('error response');
const bodyResponse = {
  response: {
    body: {
      errors: [
        'array response body error #1',
        'array response body error #2',
      ]
    }
  }
}

class ApiErrorsExample extends React.Component {
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
      <div className="demo">
        <ApiErrors
          response={errorResponse}
          sanitizeError={error => error + '!'}
        />

        {this.state.errorsVisible &&
          <ApiErrors
            response={bodyResponse}
            closeAction={this.handleClose.bind(this)}
          />
        }
      </div>
    );
  }
}

<ApiErrorsExample />
```
