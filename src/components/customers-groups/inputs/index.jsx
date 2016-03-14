import plain from './plain';
import oneOf from './one-of';

export default {
  plain,
  oneOf,
};
/*
 1. text - for text
 2. textOneOf - for text, VerticalSelect
 3. currency - CurrencyInput
 4. currencyOneOf - CurrencyInput, VerticalSelect
 5. currencyRange - 2 CurrencyInput
 6. choice - dropdown with items from config choices
 7. stateLookup - lookup for regions, as dropdown - DropdownTypeahead
 8. stateLookupOneOf - lookup for regions, as dropdown - DropdownTypeahead, with VerticalSelect
 9. cityLookup - lookup for regions, as dropdown - DropdownTypeahead
 10. cityLookupOneOf - lookup for regions, as dropdown - DropdownTypeahead, with VerticalSelect
 11. number - plain input with type number
 12. numberOneOf - plain input with type number, VerticalSelect
 13. date - date picker
 14. dateOneOf - date picker, VerticalSelect
 15. dateRange - 2 date pickers
 range - specific composite field, that uses given input and creates wrapper
 */

//field:
//const text = ({criterion, value, onChange}) => {
//  return (
//    <FormField>
//      <input name={criterion.field}
//             maxLength='255'
//             type='text'
//             onChange={({target}) => onChange(target.value)}
//             value={value} />
//    </FormField>
//  );
//};
//
//const dropdown = ({criterion, value, onChange}) => {
//  return (
//    <FormField label="Country">
//      <Dropdown name="countryId"
//                value={this.state.countryId}
//                onChange={value => this.handleCountryChange(Number(value))}>
//        {this.countryItems}
//      </Dropdown>
//    </FormField>
//  );
//};
