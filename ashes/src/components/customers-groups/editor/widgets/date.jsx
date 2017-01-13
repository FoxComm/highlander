//components
import { Input, getDefault, isValid } from '../inputs/date';
import { Label } from '../labels/date';

export const storedDateFormat = 'YYYY-MM-DDTHH:mm:ss.SSSZ';
export const labelDateFormat = 'MM/DD/YYYY';

export default {
  Input,
  getDefault,
  isValid,
  Label,
};
