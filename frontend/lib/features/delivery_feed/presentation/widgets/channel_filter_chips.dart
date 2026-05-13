import 'package:flutter/material.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';

class ChannelFilterChips extends StatelessWidget {
  const ChannelFilterChips({
    required this.selected,
    required this.onSelected,
    super.key,
  });

  final ChannelTypeEnum? selected;
  final void Function(ChannelTypeEnum?) onSelected;

  @override
  Widget build(BuildContext context) {
    const filters = [
      (null, 'All'),
      (ChannelTypeEnum.slack, 'Slack'),
      (ChannelTypeEnum.telegram, 'Telegram'),
      (ChannelTypeEnum.discord, 'Discord'),
    ];

    return SizedBox(
      height: 50,
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.s16),
        child: Row(
          children: filters.map((filter) {
            final isSelected = selected == filter.$1;
            return Padding(
              padding: const EdgeInsets.only(right: AppSpacing.s8),
              child: FilterChip(
                label: Text(filter.$2),
                selected: isSelected,
                onSelected: (_) => onSelected(filter.$1),
                backgroundColor: AppColors.surface,
                selectedColor: AppColors.primary,
                labelStyle: AppTextStyles.labelMedium.copyWith(
                  color: isSelected ? Colors.white : AppColors.textSecondary,
                ),
                side: BorderSide(
                  color: isSelected ? AppColors.primary : AppColors.divider,
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }
}
