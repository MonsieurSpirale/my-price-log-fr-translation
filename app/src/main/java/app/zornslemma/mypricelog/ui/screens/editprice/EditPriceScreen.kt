package app.zornslemma.mypricelog.ui.screens.editprice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.zornslemma.mypricelog.R
import app.zornslemma.mypricelog.common.formatPercent
import app.zornslemma.mypricelog.data.DataSet
import app.zornslemma.mypricelog.data.EditablePrice
import app.zornslemma.mypricelog.data.Item
import app.zornslemma.mypricelog.data.unitPrice
import app.zornslemma.mypricelog.debug.myCheck
import app.zornslemma.mypricelog.domain.MeasurementUnit
import app.zornslemma.mypricelog.domain.QuantityType
import app.zornslemma.mypricelog.domain.UnitPrice
import app.zornslemma.mypricelog.domain.areDifferentUnitFamilies
import app.zornslemma.mypricelog.domain.baseUnit
import app.zornslemma.mypricelog.domain.calculateFriendlyUnitPriceDenominator
import app.zornslemma.mypricelog.domain.getRelevantMeasurementUnits
import app.zornslemma.mypricelog.ui.common.AsyncOperationStatus
import app.zornslemma.mypricelog.ui.common.isNotBusy
import app.zornslemma.mypricelog.ui.components.FilteredTextField
import app.zornslemma.mypricelog.ui.components.LabelledSwitch
import app.zornslemma.mypricelog.ui.components.LabeledItem
import app.zornslemma.mypricelog.ui.components.MyExposedDropdownMenuBox
import app.zornslemma.mypricelog.ui.components.SharedViewModel
import app.zornslemma.mypricelog.ui.components.UnitPriceField
import app.zornslemma.mypricelog.ui.components.ValidatedNumericTextField
import app.zornslemma.mypricelog.ui.components.createOnCandidateValueChangeMaxLength
import app.zornslemma.mypricelog.ui.components.generaledit.GeneralEditScreen
import app.zornslemma.mypricelog.ui.components.keyboardCapitalization
import app.zornslemma.mypricelog.ui.components.rememberSyncedTextFieldValue
import app.zornslemma.mypricelog.ui.components.selectUnitPriceDenominator
import app.zornslemma.mypricelog.ui.components.textOrNull
import app.zornslemma.mypricelog.ui.components.topAppBarTitle
import app.zornslemma.mypricelog.ui.maxNotesLength
import app.zornslemma.mypricelog.ui.nonBreakingSpace
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPriceScreen(
    sharedViewModel: SharedViewModel,
    viewModel: EditPriceViewModel,
    requestClose: (Long?) -> Unit,
) {
    val originalPrice = viewModel.uiContent.originalContent
    val editablePrice by viewModel.uiContent.editableContent.collectAsStateWithLifecycle()
    val dataSet = viewModel.uiContent.staticContent.dataSet
    val item = viewModel.uiContent.staticContent.item
    val source = viewModel.uiContent.staticContent.source
    val autoUnitPriceDenominator = viewModel.uiContent.staticContent.autoUnitPriceDenominator
    val nonLinearEdit = viewModel.uiContent.staticContent.nonLinearEdit
    val locale = viewModel.uiContent.staticContent.frozenLocale

    val saveStatus by
        viewModel.generalEditScreenStateHolder.asyncOperationStatus.collectAsStateWithLifecycle()

    fun onPackSizeOrPriceChange() {
        // On the first change to the pack size or price, we set the "to confirm" switch to true, on
        // the grounds that if the user is changing these values, they must be getting them from
        // somewhere and the assumption is that they have the actual current price/pack in front of
        // them. (We don't do this if they edit the notes; it's conceivable they are for example
        // trying the product at home and making a note that a certain brand isn't very nice and not
        // to consider it as acceptable in future.) We only do this on the first change so we don't
        // fight with the user if they toggle this back off afterwards.
        // ENHANCE: We might want to gate this logic behind a Settings option, i.e. have an option
        // to let the confirm always stay off unless the user explicitly turns it on. That said, in
        // my own personal use, this logic seems to work well.
        if (!viewModel.firstPackSizeOrPriceChangeOccurred) {
            // Note that we must not use the captured editablePrice here, because this function is
            // likely to be called after editableContent has been changed but before a
            // recomposition has updated editablePrice and this function has been re-defined.
            viewModel.setUiContentEditablePrice(
                viewModel.uiContent.editableContent.value.copy(toConfirm = true)
            )
            viewModel.firstPackSizeOrPriceChangeOccurred = true
        }
    }

    GeneralEditScreen(
        stateHolder = viewModel.generalEditScreenStateHolder,
        title = topAppBarTitle(item.name, source.name),
        isDirty = {
            editablePrice.copy(toConfirm = false) != originalPrice.copy(toConfirm = false)
        },
        validateForSave = { viewModel.validateForSave() },
        performSave = { viewModel.performSave() },
        onIdle = {},
        requestClose = requestClose,
    ) {
        // We put the price above the pack size. This matches the order we show things (at least in
        // English) on the read-only home screen. It also ties in with the price usually being the
        // primary item on a shelf label. ENHANCE: If anyone expresses an interest, we could make
        // the ordering of these translation-configurable. (Don't forget to alter the order we
        // check for validation failures to match, as well as re-ordering the actual composables
        // here.)

        EditPriceScreenPrice(viewModel, editablePrice, ::onPackSizeOrPriceChange)

        Spacer(modifier = Modifier.height(16.dp))

        EditPriceScreenPackSize(viewModel, editablePrice, dataSet, item, ::onPackSizeOrPriceChange)

        Spacer(modifier = Modifier.height(16.dp))

        EditPriceScreenUnitPriceRow(
            sharedViewModel,
            originalPrice,
            editablePrice,
            dataSet,
            autoUnitPriceDenominator,
            nonLinearEdit,
            locale,
            saveStatus,
        )

        // We don't show the switch if this is the first price for an item and source; the price is
        // confirmed, otherwise why are we entering it? Note that this is not the same as id being
        // 0, because if we deleted the price and are re-creating it from the history, we have no
        // ID but toConfirm will be false so we can preserve the old confirmation date by default.
        if (!originalPrice.toConfirm) {
            Spacer(modifier = Modifier.height(16.dp))

            LabelledSwitch(
                title = stringResource(R.string.label_confirm_pack_size_and_price),
                supportingText = stringResource(R.string.supporting_text_details_correct_right_now),
                enabled = saveStatus.isNotBusy(),
                checked = editablePrice.toConfirm,
                onCheckedChange = {
                    viewModel.setUiContentEditablePrice(editablePrice.copy(toConfirm = it))
                },
            )
        } else {
            myCheck(editablePrice.toConfirm) {
                "Expected toConfirm to be true as this is the first price, but it's false"
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ENHANCE: Can/should I do something to scroll the screen when focus enters this and the
        // caret is half-hidden?
        var notes by rememberSyncedTextFieldValue(editablePrice.notes)
        FilteredTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_notes)) },
            keyboardOptions =
                KeyboardOptions(keyboardCapitalization(R.string.keyboard_capitalization_notes)),
            value = notes,
            onCandidateValueChange = createOnCandidateValueChangeMaxLength(maxNotesLength),
            onValueChange = {
                notes = it
                viewModel.setUiContentEditablePrice(editablePrice.copy(notes = it.text))
            },
            enabled = saveStatus.isNotBusy(),
        )
    }
}

@Composable
private fun EditPriceScreenPrice(
    viewModel: EditPriceViewModel,
    editablePrice: EditablePrice,
    onChange: () -> Unit,
) {
    val uiContent = viewModel.uiContent

    val saveStatus by
        viewModel.generalEditScreenStateHolder.asyncOperationStatus.collectAsStateWithLifecycle()

    var packPrice by rememberSyncedTextFieldValue(editablePrice.price)
    val currencyFormat = viewModel.currencyFormat

    ValidatedNumericTextField(
        value = packPrice,
        locale = uiContent.staticContent.frozenLocale,
        validationRules = currencyFormat.validationRules,
        // No validationRulesKey is needed as the validation rules depend only on our fixed
        // DataSet and frozen locale.
        allowEmpty = !viewModel.generalEditScreenStateHolder.saveAttempted,
        validationFlow = viewModel.saveValidationEvents,
        validationFlowFieldId = EditPriceViewModel.EditableField.PRICE,
        errorHighlightOffset = 4.dp,
        numericTextFieldModifier = Modifier.fillMaxWidth(),
        // We use the same label text here as on the home screen, since it's the same value.
        label = { Text(stringResource(R.string.label_shelf_price)) },
        prefix = textOrNull(currencyFormat.prefix),
        suffix = textOrNull(currencyFormat.suffix),
        textStyle =
            if (currencyFormat.prefix == null && currencyFormat.suffix != null)
                LocalTextStyle.current.copy(textAlign = TextAlign.End)
            else LocalTextStyle.current,
        onValueChange = {
            packPrice = it
            if (editablePrice.price != it.text) {
                viewModel.setUiContentEditablePrice(editablePrice.copy(price = it.text))
                onChange()
            }
        },
        enabled = saveStatus.isNotBusy(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    if (currencyFormat.decimalPlaces == 0) KeyboardType.Number
                    else KeyboardType.Decimal
            ),
    )
}

@Composable
private fun EditPriceScreenPackSize(
    viewModel: EditPriceViewModel,
    editablePrice: EditablePrice,
    dataSet: DataSet,
    item: Item,
    onChange: () -> Unit,
) {
    val uiContent = viewModel.uiContent
    val editableContent by uiContent.editableContent.collectAsStateWithLifecycle()

    val saveStatus by
        viewModel.generalEditScreenStateHolder.asyncOperationStatus.collectAsStateWithLifecycle()

    val units: List<MeasurementUnit> =
        remember(dataSet, item.defaultUnit.quantityType) {
            dataSet.getRelevantMeasurementUnits(
                item.defaultUnit.quantityType,
                includeDisplayOnly = false,
            )
        }
    var packCountNumber by rememberSyncedTextFieldValue(editablePrice.count)
    var packSizeNumber by rememberSyncedTextFieldValue(editablePrice.measureValue)

    if (viewModel.showPackCount) {
        ValidatedNumericTextField(
            value = packCountNumber,
            locale = uiContent.staticContent.frozenLocale,
            validationRules = viewModel.packCountValidationRules,
            allowEmpty = !viewModel.generalEditScreenStateHolder.saveAttempted,
            validationFlow = viewModel.saveValidationEvents,
            validationFlowFieldId = EditPriceViewModel.EditableField.PACK_COUNT,
            numericTextFieldModifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_count)) },
            onValueChange = {
                packCountNumber = it
                if (editablePrice.count != it.text) {
                    viewModel.setUiContentEditablePrice(editablePrice.copy(count = it.text))
                    onChange()
                }
            },
            enabled = saveStatus.isNotBusy(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    Row {
        ValidatedNumericTextField(
            value = packSizeNumber,
            locale = uiContent.staticContent.frozenLocale,
            validationRules = viewModel.packSizeValidationRules,
            validationRulesKey = editablePrice.measurementUnit.id,
            allowEmpty = !viewModel.generalEditScreenStateHolder.saveAttempted,
            validationFlow = viewModel.saveValidationEvents,
            validationFlowFieldId = EditPriceViewModel.EditableField.PACK_SIZE,
            errorHighlightOffset = 4.dp,
            baseValidatedTextFieldModifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.label_size)) },
            onValueChange = {
                packSizeNumber = it
                if (editablePrice.measureValue != it.text) {
                    viewModel.setUiContentEditablePrice(editablePrice.copy(measureValue = it.text))
                    onChange()
                }
            },
            enabled = saveStatus.isNotBusy(),
            numericTextFieldModifier = Modifier.fillMaxSize(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        if (editableContent.measurementUnit.maxDecimals == 0) KeyboardType.Number
                        else KeyboardType.Decimal
                ),
        )

        if (item.defaultUnit.quantityType != QuantityType.ITEM) {
            Spacer(modifier = Modifier.width(8.dp))

            val context = LocalContext.current
            MyExposedDropdownMenuBox(
                enabled = saveStatus.isNotBusy(),
                selectedId = editablePrice.measurementUnit.id,
                onItemSelected = {
                    val measurementUnit = MeasurementUnit.fromId(it)
                    myCheck(measurementUnit != null) {
                        "Expected non-null measurementUnit to be selected; got $it"
                    }
                    if (editablePrice.measurementUnit != measurementUnit!!) {
                        viewModel.setUiContentEditablePrice(
                            editablePrice.copy(measurementUnit = measurementUnit)
                        )
                        onChange()
                    }
                },
                label = { Text(stringResource(R.string.label_unit)) },
                items = units,
                modifier = Modifier.weight(1f),
                getId = { it.id },
                // It's generally a good thing that we use non-breaking spaces in unit symbols, but
                // here the symbol is used in isolation so there is no possibility of another space
                // allowing a natural break. So we turn the non-breaking space back into a regular
                // space. "fl"/"oz" is better than "fl o"/"z" if we are forced to wrap, although I
                // suspect we are very unlikely to need to.
                getCollapsedItemText = {
                    context.getString(it.symbol).replace(nonBreakingSpace, " ")
                },
                getItemText = {
                    "${context.getString(it.fullName)} (${context.getString(it.symbol)})"
                },
                getDividerBetween = { previousItem, item ->
                    areDifferentUnitFamilies(previousItem, item)
                },
            )
        }
    }
}

@Composable
fun EditPriceScreenUnitPriceRow(
    sharedViewModel: SharedViewModel,
    originalPrice: EditablePrice,
    editablePrice: EditablePrice,
    dataSet: DataSet,
    autoUnitPriceDenominator: MeasurementUnit,
    nonLinearEdit: Boolean,
    locale: Locale,
    saveStatus: AsyncOperationStatus,
) {
    // ENHANCE: It's possible we should use the three-field layout even when we're adding a new
    // price and there is no old price or percentage change to show - just leaving the unused
    // fields empty or with a "-" or something. It seems a bit silly to complicate the UI with these
    // redundant fields, but it may look better in practice (the single unit price field looks a
    // bit of an orphan) and there may be a consistency argument too.

    // ENHANCE: Should we have a slight delay in updating the unit price when the user is typing
    // into the contributing text fields? It might be less distracting, but it might also make the
    // app feel laggy.

    Row(modifier = Modifier.fillMaxWidth()) {
        val userUnitPriceDenominator by
            sharedViewModel.userUnitPriceDenominatorFlow.collectAsStateWithLifecycle()

        // If we're adding a new price we obviously don't have an original unit price. We also
        // don't really have one if this is a non-linear edit. We could in theory take the
        // current price if there is one, but I'm not sure it's particularly useful.
        // originalPrice is the starting copy of the historical price, which feels confusing to
        // show here.
        val haveOriginalUnitPrice = originalPrice.id != 0L && !nonLinearEdit

        val originalUnitPrice =
            if (!haveOriginalUnitPrice) null
            else
                originalPrice
                    .unitPrice(locale)
                    ?.withDenominator(
                        selectUnitPriceDenominator(
                            autoUnitPriceDenominator,
                            userUnitPriceDenominator,
                        )
                    )
        val newUnitPrice = editablePrice.unitPrice(locale)
        val currentUnitPriceDenominator =
            remember(
                userUnitPriceDenominator,
                originalUnitPrice,
                newUnitPrice,
                dataSet,
                editablePrice.measurementUnit,
            ) {
                if (userUnitPriceDenominator != null) userUnitPriceDenominator!!
                else {
                    originalUnitPrice?.denominator
                        ?: if (newUnitPrice != null) {
                            calculateFriendlyUnitPriceDenominator(
                                dataSet,
                                editablePrice.measurementUnit,
                                listOf(newUnitPrice),
                            )
                        } else {
                            null
                        }
                }
            }

        @Composable
        fun LocalUnitPriceField(unitPrice: UnitPrice?, label: String) {
            UnitPriceField(
                unitPrice = unitPrice,
                dataSet = dataSet,
                enabled = saveStatus.isNotBusy(),
                label = label,
                modifier = Modifier.weight(3f),
                onItemSelected = { sharedViewModel.updateUserUnitPriceDenominator(it) },
            )
        }

        if (originalUnitPrice != null) {
            LocalUnitPriceField(
                unitPrice = originalUnitPrice.withDenominator(currentUnitPriceDenominator!!),
                label = stringResource(R.string.label_existing_unit_price),
            )
        }

        LocalUnitPriceField(
            unitPrice = newUnitPrice?.withDenominator(currentUnitPriceDenominator!!),
            label =
                stringResource(
                    if (originalUnitPrice == null) R.string.label_unit_price
                    else R.string.label_new_unit_price
                ),
        )

        if (originalUnitPrice != null) {
            LabeledItem(
                label = stringResource(R.string.label_change),
                modifier = Modifier.weight(2f),
            ) {
                val baseUnit = originalPrice.measurementUnit.quantityType.baseUnit()
                if (newUnitPrice == null) {
                    Text("")
                } else {
                    val ratio =
                        newUnitPrice.withDenominator(baseUnit).numerator /
                            originalUnitPrice!!.withDenominator(baseUnit).numerator
                    val relativeChange = (ratio - 1)
                    // ENHANCE: It might be nice to colour code the percentage change, e.g. red for
                    // positive (bad) and green for negative (good). However, elsewhere we would
                    // have liked to use red/green in this way but it didn't feel like a good fit
                    // with Material You. We could use the "error" colour for bad and the primary
                    // colour for good, but maybe this is confusing.
                    Text(
                        formatPercent(
                            relativeChange,
                            1,
                            1,
                            true,
                            locale,
                            useLeadingPlus = abs(relativeChange) >= 0.0005,
                        )
                    )
                }
            }
        }
    }
}
