# -*- coding: utf-8 -*-
# Generated by Django 1.10.3 on 2016-12-13 20:53
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('mydatabase', '0003_auto_20161207_2025'),
    ]

    operations = [
        migrations.AddField(
            model_name='trip',
            name='data_file',
            field=models.FileField(default='', upload_to='documents'),
            preserve_default=False,
        ),
    ]
