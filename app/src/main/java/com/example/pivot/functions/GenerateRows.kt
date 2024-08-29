package com.example.pivot.functions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Generates rows for the pivot table
@Composable
fun generateRows(
    data: List<SalesData>,
    rowHeaders: List<String>,
    columnHeaderValues: List<String>,
    nestedHeaderValuesByColumn: Map<String, List<String?>>,
    thirdHeaderValuesByNestedHeader: Map<Pair<Any?, Any?>, List<String?>>,
    valuesField: String,
    columnHeaderName: String,
    nestedHeaderName: String,
    thirdHeaderName: String
) {
    val groupedData = groupByHeaders(data, rowHeaders)

    // Track colors for cell values
    val valueColors = mutableMapOf<Any?, Color>()

    // First pass to determine colors based on total sales amount
    groupedData.forEach { (firstHeaderValue, firstGroup) ->
        columnHeaderValues.forEach { category ->
            nestedHeaderValuesByColumn[category]?.forEach { productName ->
                thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.forEach { quality ->
                    val totalSalesAmount = firstGroup.filter {
                        getPropertyValue<SalesData>(it, columnHeaderName) == category &&
                                getPropertyValue<SalesData>(it, nestedHeaderName) == productName &&
                                getPropertyValue<SalesData>(it, thirdHeaderName) == quality
                    }.sumOf { getPropertyValue<SalesData>(it, valuesField) as Double }

                    val displayText = if (totalSalesAmount == 0.0) "-" else totalSalesAmount.toString()
                    valueColors[displayText] = Color(0xFFFFC0CB) // Set color for the value
                }
            }
        }
    }

    // Second pass to render rows with determined colors
    groupedData.forEach { (firstHeaderValue, firstGroup) ->
        val backgroundColor = Color(0xFFFFC0CB) // Color for the first row

        Row(Modifier.fillMaxWidth()) {
            TableCell(text = firstHeaderValue.toString(), width = 120.dp, backgroundColor = backgroundColor)

            columnHeaderValues.forEach { category ->
                nestedHeaderValuesByColumn[category]?.forEach { productName ->
                    if (columnHeaderName.isNotEmpty()) {
                        thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.forEach { quality ->
                            val totalSalesAmount = firstGroup.filter {
                                getPropertyValue<SalesData>(it, columnHeaderName) == category &&
                                        getPropertyValue<SalesData>(it, nestedHeaderName) == productName &&
                                        getPropertyValue<SalesData>(it, thirdHeaderName) == quality
                            }.sumOf { getPropertyValue<SalesData>(it, valuesField) as Double }

                            val displayText = if (totalSalesAmount == 0.0) "-" else totalSalesAmount.toString()
                            val cellColor = valueColors[displayText] ?: Color.Transparent
                            TableCell(text = displayText, width = 120.dp, backgroundColor = cellColor)
                        }
                    }
                }
            }
        }

        generateSubRows(
            firstGroup,
            rowHeaders.drop(1),
            columnHeaderValues,
            nestedHeaderValuesByColumn,
            thirdHeaderValuesByNestedHeader,
            valuesField,
            columnHeaderName,
            nestedHeaderName,
            thirdHeaderName
        )
    }
}

// Generates sub-rows for the pivot table
@Composable
fun generateSubRows(
    data: List<SalesData>,
    rowHeaders: List<String>,
    columnHeaderValues: List<String>,
    nestedHeaderValuesByColumn: Map<String, List<String?>>,
    thirdHeaderValuesByNestedHeader: Map<Pair<Any?, Any?>, List<String?>>,
    valuesField: String,
    columnHeaderName: String,
    nestedHeaderName: String,
    thirdHeaderName: String
) {
    if (rowHeaders.isEmpty()) return

    val groupedData = groupByHeaders(data, rowHeaders)

    // Define colors for cell values
    val valueColors = mutableMapOf<String, Color>()
    if (rowHeaders.size == 1) {
        valueColors["default"] = Color.Yellow
    }

    groupedData.forEach { (headerValue, group) ->
        val isFirstRow = rowHeaders.size == 1 // Determine if it's the first row in the remaining headers
        val backgroundColor = if (isFirstRow) Color.Yellow else Color.Transparent

        Row(Modifier.fillMaxWidth()) {
            TableCell(text = headerValue.toString(), width = 120.dp, backgroundColor = backgroundColor)

            // Calculate and display totals for each combination
            columnHeaderValues.forEach { category ->
                nestedHeaderValuesByColumn[category]?.forEach { productName ->
                    thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.forEach { quality ->
                        val totalSalesAmount = group.filter {
                            getPropertyValue<SalesData>(it, columnHeaderName) == category &&
                                    getPropertyValue<SalesData>(it, nestedHeaderName) == productName &&
                                    getPropertyValue<SalesData>(it, thirdHeaderName) == quality
                        }.sumOf { getPropertyValue<SalesData>(it, valuesField) as Double }

                        val displayText = if (totalSalesAmount == 0.0) "-" else totalSalesAmount.toString()

                        // Determine cell color based on value
                        val cellColor = if (rowHeaders.size == 1) {
                            valueColors["default"] ?: Color.Transparent
                        } else {
                            Color.Transparent
                        }

                        TableCell(text = displayText, width = 120.dp, backgroundColor = cellColor)
                    }
                }
            }
        }

        // Recursively generate sub-rows
        generateSubRows(
            group,
            rowHeaders.drop(1),
            columnHeaderValues,
            nestedHeaderValuesByColumn,
            thirdHeaderValuesByNestedHeader,
            valuesField,
            columnHeaderName,
            nestedHeaderName,
            thirdHeaderName
        )
    }
}

// Groups data by specified headers
fun groupByHeaders(data: List<SalesData>, headers: List<String>): Map<Any?, List<SalesData>> {
    if (headers.isEmpty()) return emptyMap()

    val header = headers.first()
    return data.groupBy { getPropertyValue(it, header) }
}
