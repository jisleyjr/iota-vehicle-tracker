# Payments tab and cost per mile

Design mockups for a third vehicle tab and a cost-of-ownership figure. Nothing in `app/src/main` changes, and the Room schema is unchanged. The screens are Compose previews under `app/src/test/java/com/example/design/payments/`, drawn with the app theme. The garage cards, the efficiency card, the fill-up rows, and the dialog shell are the components the app already uses. The Camry in these images is the same 2021 Toyota Camry (`CAM4219`) as the screenshots in `docs/screenshots/`, with the same fuel total ($162.08) and the same service total ($389.50).

Roborazzi records the PNGs in this folder from `PaymentsMockupScreenshotTest` on a Pixel 8.

```bash
./gradlew :app:recordRoborazziDebug --tests com.example.design.payments.PaymentsMockupScreenshotTest
```

`./gradlew testDebugUnitTest` compares those tests with the PNGs here.

## What the numbers mean

Total cost is cash already spent on the vehicle, plus fuel, plus maintenance.

- A loan or a lease uses the logged payments, including a down payment. The agreed price is kept on the breakdown so the owner can see it, and it is not added a second time.
- A vehicle with a purchase price and no loan or lease uses that purchase price once.
- Fuel is the sum of gallons times price per gallon on fill-ups. That is the same figure as Gas Costs.
- Maintenance is the sum of service costs. That is the same figure as Service Repairs.
- The efficiency card's Total Expenditures stays fuel plus service. These mockups do not replace that $551.58 tile.

Cost per mile is total cost divided by miles driven, shown in cents to one decimal place. Miles driven are the latest odometer (the highest reading on a fill-up, a service record, or a payment) minus the odometer at purchase. When no purchase odometer is logged, miles driven are the sum of trip miles. A vehicle with no miles yet would show an em dash, the same way Last Oil Change does.

Remaining balance is the schedule minus cash already logged. The schedule in these mockups is the down payment plus the monthly amount times the term. Unpaid installments are not part of total cost, and they are not part of the cents-per-mile figure, until the owner logs them. Hiding the Payments tab does not delete payments. The logged amounts still count.

The Camry story used in every image:

| Piece | Amount |
| --- | --- |
| Agreed price (reference only) | $24,800.00 |
| Down payment, 03/12/2024, odometer 68,000 | $3,000.00 |
| Loan | $498.00 a month for 48 months |
| Payments logged | 4, newest on 11/01/2026 |
| Cash paid | $4,992.00 |
| Schedule total | $26,904.00 |
| Remaining | $21,912.00 |
| Fuel | $162.08 |
| Maintenance | $389.50 |
| Total cost | $5,543.58 |
| Miles driven | 17,399 (85,399 − 68,000) |
| Cost per mile | 31.9¢ |

The form is the same on every option. Type chips are Loan payment, Lease payment, Down payment, and Purchase price. A new row can be edited later, and Edit includes Delete payment, matching Edit Maintenance.

## Option A — tab row and a loan progress card

A Material 3 tab row reads Fill-ups, Service Logs, and Payments. A strip above the tabs always shows total cost and cost per mile, including after the Payments tab is gone. The Payments list opens with a loan progress card (monthly amount, term, remaining, payments logged, next due, and a note that the agreed price is not added on top). The list keeps the down payment above the installments. The ⋮ menu on the Payments header has Cost breakdown and Mark paid off and hide tab. Confirming hides the tab and leaves two tabs over the fill-up list. The cost strip stays.

**Works well:** The third tab has room for a full label. The progress card answers "how much is left" without opening another screen. Total cost and cents per mile survive hiding the tab. The hide action sits on the Payments header, next to the list it removes.

**Costs:** The tab row is a second navigation style beside today's filled pill. The strip and the progress card both talk about money. The tabs are text only, while today's tabs have icons.

## Option B — segmented buttons and a Paid off chip

Material 3 segmented buttons are Fill-ups, Service Logs, and Payments. The Payments page leads with three small figures: total cost, per mile, and remaining, then the same payment list. There is no tall progress card, so more rows fit. Mark paid off and hide Payments lives on the Camry card's ⋮ menu, above Delete vehicle, with Cost breakdown. After confirm, the third segment is gone. A Paid off chip sits beside the two remaining segments, and a green bar under them keeps the total and the cents per mile. The fill-up list is the page underneath.

**Works well:** The third choice is the same kind of control as the first two. The vehicle menu is where Delete vehicle already lives, so payoff is a property of the car. The chip is an obvious "this loan is done" marker, and the bar under it keeps the two headline numbers.

**Costs:** Three labels in one segmented control are tight. The hidden state shows both a chip and a total bar. The open menu covers part of the efficiency card. The hide action is not on the Payments list itself.

## Option C — dashboard tiles, then the Payments tab

A rose tile row under the efficiency card shows Total cost, Per mile, and Remaining, in the same mini-card style as the expanded dashboard. The tab control is the app's filled pill extended to Fill-ups, Service Logs, and Payments. The list is the down payment and the installments. Hiding is a setting in Vehicle details: a Show Payments tab switch, plus Mark paid off and hide tab, then the same confirmation. After hiding, the third tile reads Paid off, and the tab control is the current two-tab pill (Gas Fill-ups and Service Logs) with the fill-up list under it. Total cost and cents per mile stay in the tiles.

**Works well:** The numbers the owner wants are on the dashboard for every tab, and they remain after the Payments tab is gone. The hidden screen uses today's exact tab control. The show-or-hide switch has a home in vehicle details, separate from Delete vehicle. The selected Payments segment matches the current pill, including the icon.

**Costs:** The tile row is on screen even while the owner is logging gas. Remaining balance can be read as money already spent if the label is missed. Term, next due, and the agreed-price note are not on the list. They are on the cost breakdown.

## Option D — logged payments, figures inside the expandable dashboard

Options A–C stay as drawn, including their loan schedule, remaining balance, and paid-off wording. Option D drops loan tracking. A payment is a logged amount: one Purchase for a vehicle bought outright, or a series of Down payment, Payment, and Other rows. The owner stops adding rows when they are finished. There is no loan amount, term, monthly payment, remaining balance, progress card, or next due date.

Paid to date is the sum of those rows ($4,992.00 for this Camry). Total cost is still paid to date plus fuel plus maintenance ($5,543.58), and cost per mile is still 31.9¢.

Collapsed, the efficiency card is only 33.1 MPG and 2399 miles since the oil change. The filled pill is Fill-ups, Service Logs, and Payments. The list is the logged rows. The Payments ⋮ menu includes Hide Payments tab. No total, cents per mile, or paid-to-date figure is on that screen.

Expanded, the card matches the current dashboard: Total Expenditures, Current Mileage, Gas Costs, Service Repairs, and the tire sets. One more row holds Total cost ($5,543.58), Per mile (31.9¢), and Paid to date ($4,992.00).

The add form is amount, date, type (Purchase, Down payment, Payment, Other), and an optional note. The hide confirmation says "Hide Payments tab?" and "Your logged payments stay in total cost."

After the tab is hidden, the pill is today's Gas Fill-ups and Service Logs control. Collapsed, that is the current fill-up list. Expanded, the same three tiles remain: Total cost, Per mile, and Paid to date. There is no Paid off tile.

**Works well:** The list stays as compact as today's screen. The owner opens the figures the same way they already open Gas Costs and tire sets. Hiding the tab does not throw away the logged amounts, and it does not invent a payoff state.

**Costs:** Total cost and cents per mile are easy to miss while the card is collapsed. Expanding the card is tall, so the tab row sits at the bottom edge of the screen, under the add button. Paid to date is a third column, so that label is narrower than Gas Costs.

## Cost breakdown

`total-cost-breakdown.png` is the detail screen for this direction. It shows 31.9¢ per mile, the stacked split of payments, fuel, and maintenance, the $5,543.58 total, and the miles. Payments are about 90% of what this Camry has cost so far, because $4,992.00 is logged against 17,399 miles. There is no agreed price and no remaining balance on this screen. Options A–C still mention those ideas in their own images.

## A direction worth building

Follow Option D. Payments are logged entries with types Purchase, Down payment, Payment, and Other. Keep total cost, cents per mile, and paid to date inside the expandable efficiency card, collapsed by default, and leave them off the list. Use today's filled pill until the owner chooses Hide Payments tab. The confirmation tells them the logged payments stay in the total. Options A, B, and C remain in this folder as the earlier studies that put a summary on the list and tracked a loan.

## Images

| File | What it shows |
| --- | --- |
| `option-a-payments-tab.png` | Option A. Tab row with Payments selected, the total-cost and 31.9¢ strip, the loan progress card, and the down payment above the installments. |
| `option-a-add-payment.png` | Option A. Log Payment for a $498.00 loan installment on 11/13/2026, with Loan, Lease, Down payment, and Purchase price. |
| `option-a-hide-payments-menu.png` | Option A. The Payments ⋮ menu with Cost breakdown and Mark paid off and hide tab. |
| `option-a-hide-payments-confirm.png` | Option A. Confirmation that hiding the tab keeps the $4,992.00 already logged in the $5,543.58 total and the 31.9¢ per mile. |
| `option-a-payments-hidden.png` | Option A after hiding. Two tabs (Fill-ups and Service Logs), the cost strip still showing $5,543.58 and 31.9¢, and the Camry fill-up list. |
| `option-b-payments-tab.png` | Option B. Segmented buttons with Payments selected, a three-figure summary (total, per mile, remaining), and the payment list. |
| `option-b-edit-payment.png` | Option B. Edit Payment for the November installment, prefilled, with Delete payment. |
| `option-b-hide-payments-menu.png` | Option B. The Camry card menu with Cost breakdown, Mark paid off and hide Payments, and Delete vehicle. |
| `option-b-hide-payments-confirm.png` | Option B. The same hide confirmation, pointing back to the vehicle menu. |
| `option-b-payments-hidden.png` | Option B after hiding. Two segments, a Paid off chip, a bar with the total and 31.9¢/mi, and the fill-up list. |
| `option-c-payments-tab.png` | Option C. Dashboard tiles for total cost, per mile, and remaining, the filled pill extended with Payments, and the payment list. |
| `option-c-add-payment.png` | Option C. Log Payment with Down payment selected, $3,000.00, odometer 68,000, date 03/12/2024. |
| `option-c-vehicle-details.png` | Option C. Vehicle details for the Camry, with Show Payments tab on and Mark paid off and hide tab. |
| `option-c-hide-payments-confirm.png` | Option C. The hide confirmation, pointing back to vehicle details. |
| `option-c-payments-hidden.png` | Option C after hiding. Tiles still show $5,543.58 and 31.9¢, the third tile reads Paid off, and the tabs are the current Gas Fill-ups and Service Logs pill. |
| `option-d-collapsed.png` | Option D collapsed. Filled pill with Payments selected and the logged payment rows. The efficiency card stays on MPG and last oil change. No total, cents per mile, or paid to date. |
| `option-d-expanded.png` | Option D with the dashboard expanded. Existing expenditure tiles and tire sets, plus Total cost $5,543.58, Per mile 31.9¢, and Paid to date $4,992.00. |
| `option-d-payments-hidden-expanded.png` | Option D after hiding Payments, dashboard expanded. Two tabs, with the same Total cost, Per mile, and Paid to date tiles. |
| `option-d-payments-hidden-collapsed.png` | Option D after hiding Payments, dashboard collapsed. Today's two-tab fill-up screen, with no ownership figures. |
| `option-d-add-payment.png` | Option D Log Payment form: type chips for Purchase, Down payment, Payment, and Other, plus amount, date, and an optional note. |
| `option-d-hide-confirm.png` | Option D confirmation: "Hide Payments tab?" and "Your logged payments stay in total cost." |
| `total-cost-breakdown.png` | Cost of ownership for the Camry: 31.9¢ per mile, payments, fuel, maintenance, the total, and the miles. |
