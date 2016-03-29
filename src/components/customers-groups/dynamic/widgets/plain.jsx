//components
import { Input, getDefault } from '../inputs/plain';
import { Label } from '../labels/plain';


export default function (type) {
  return {
    Input: Input(type),
    getDefault: getDefault(type),
    Label: Label(type),
  };
}
