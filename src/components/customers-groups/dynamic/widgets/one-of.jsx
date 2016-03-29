//components
import { Input } from '../inputs/one-of';
import { Label } from '../labels/one-of';


export default function(Widget) {
  return {
    Input: Input(Widget),
    Label: Label(Widget)
  };
}
