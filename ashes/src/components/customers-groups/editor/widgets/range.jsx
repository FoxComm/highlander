//components
import { Input, getDefault, isValid } from '../inputs/range';
import { Label } from '../labels/range';

export default function(Widget) {
  return {
    Input: Input(Widget),
    getDefault: getDefault(Widget),
    isValid: isValid(Widget),
    Label: Label(Widget),
  };
}
