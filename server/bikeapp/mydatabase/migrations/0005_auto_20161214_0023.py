# -*- coding: utf-8 -*-
# Generated by Django 1.10.3 on 2016-12-13 23:23
from __future__ import unicode_literals

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('mydatabase', '0004_trip_data_file'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='tripdetail',
            name='trip_fkey',
        ),
        migrations.DeleteModel(
            name='TripDetail',
        ),
    ]
