# -*- coding: utf-8 -*-
# Generated by Django 1.10.3 on 2017-01-14 19:07
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('mydatabase', '0005_auto_20161214_0023'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='trip',
            name='data_file',
        ),
        migrations.AddField(
            model_name='trip',
            name='trip_data',
            field=models.TextField(default='asdad'),
            preserve_default=False,
        ),
    ]
